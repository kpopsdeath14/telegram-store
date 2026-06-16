(ns {{name}}.services.cdek
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [clojure.string :as s]
   [{{name}}.datamodule :as dm]))

(defonce suppress-cookie-warnings
  (doto (java.util.logging.Logger/getLogger "org.apache.http.client.protocol.ResponseProcessCookies")
    (.setLevel java.util.logging.Level/OFF)))

(def cdek-prod-url "https://api.cdek.ru")
(def cdek-test-url "https://api.edu.cdek.ru")

(def cdek-token-cache (atom {:access_token nil
                             :token_type "Bearer"
                             :expires_at 0}))

(defn now-ms []
  (System/currentTimeMillis))

(defn parse-bool [value]
  (cond
    (boolean? value) value
    (string? value) (contains? #{"true" "1" "yes" "y"} (s/lower-case value))
    :else false))

(defn connection-attr [attrs key]
  (let [string-key (name key)]
    (or (get attrs key)
        (get attrs string-key))))

(defn cdek-provider []
  (let [providers (mapv (fn [product] (:data product))
                        (dm/db_query_sender "" dm/order_delivery_provider_get_sql {}))]
    (some (fn [item]
            (when (= (:delivery_provider_name item) "cdek")
              item))
          providers)))

(defn cdek-base-url []
  (let [provider (cdek-provider)
        attrs (:connection_attributes provider)
        explicit (or (connection-attr attrs :cdek_base_url)
                     (connection-attr attrs :api_base_url)
                     (connection-attr attrs :base_url))
        mode (some-> (or (connection-attr attrs :api_mode)
                         (connection-attr attrs :mode))
                     str s/lower-case)
        test-mode (or (parse-bool (connection-attr attrs :test_mode))
                      (parse-bool (connection-attr attrs :cdek_test_mode))
                      (= mode "test"))]
    (cond
      (and (string? explicit) (not (s/blank? explicit))) explicit
      test-mode cdek-test-url
      :else cdek-prod-url)))

(defn cdek-credentials []
  (let [provider (cdek-provider)
        attrs (:connection_attributes provider)
        account (connection-attr attrs :account)
        secure-password (connection-attr attrs :secure_password)]
    {:account account
     :secure_password secure-password
     :provider provider}))

(defn cdek-token-valid? [token]
  (and (:access_token token)
       (:expires_at token)
       (> (:expires_at token) (+ (now-ms) 60000))))

(defn cdek-auth-request [account secure-password]
  (http/post (str (cdek-base-url) "/v2/oauth/token")
             {:form-params {:grant_type "client_credentials"
                            :client_id account
                            :client_secret secure-password}
              :content-type :x-www-form-urlencoded
              :accept :json
              :as :json
              :cookie-policy :ignore
              :throw-exceptions false}))

(defn cdek-store-token! [body]
  (let [access-token (:access_token body)
        token-type (or (:token_type body) "Bearer")
        expires-in (or (:expires_in body) 0)
        expires-at (+ (now-ms) (* 1000 expires-in))
        token {:access_token access-token
               :token_type token-type
               :expires_in expires-in
               :expires_at expires-at}]
    (when access-token
      (reset! cdek-token-cache token)
      token)))

(defn cdek-token-get [force?]
  (let [cached @cdek-token-cache]
    (if (and (not force?) (cdek-token-valid? cached))
      {:ok true :token cached :cached? true}
      (let [{:keys [account secure_password]} (cdek-credentials)]
        (if (or (s/blank? account) (s/blank? secure_password))
          (do
            (println "[CDEK] token_get missing credentials:"
                     "account=" (pr-str account)
                     "secure_password=" (pr-str secure_password))
            {:ok false :status 400 :error "CDEK credentials are not configured"})
          (let [response (cdek-auth-request account secure_password)
                body (:body response)
                token (when (map? body) (cdek-store-token! body))]
            (if token
              {:ok true :token token :cached? false :raw response}
              (do
                (println "[CDEK] token_get auth failed:"
                         "account=" (pr-str account)
                         "secure_password=" (pr-str secure_password)
                         "response=" (pr-str body))
                {:ok false :status (:status response) :error body}))))))))

(defn cdek-auth-header [token]
  (str (or (:token_type token) "Bearer") " " (:access_token token)))

(defn cdek-request [method path token {:keys [body query]}]
  (http/request (cond-> {:method method
                         :url (str (cdek-base-url) path)
                         :headers {"Authorization" (cdek-auth-header token)}
                         :content-type :json
                         :accept :json
                         :as :json
                         :cookie-policy :ignore
                         :throw-exceptions false}
                  body (assoc :body (json/write-str body))
                  query (assoc :query-params query))))

(defn cdek-delivery-points [req]
  (let [req-body (:params req)
        query (or (:cdek_query req-body) (:query req-body) {})
        search-text (or (:search_text req-body)
                        (:search-text req-body)
                        (:search_text query)
                        (:search-text query)
                        (:name query)
                        (get query "name")
                        "")
        city-code (or (:city_code req-body)
                      (:city-code req-body)
                      (:city_code query)
                      (:city-code query)
                      (get query "city_code"))
        type-val (or (:type query) (get query "type"))
        token-result (cdek-token-get false)]
    (if-not (:ok token-result)
      {:status (or (:status token-result) 500)
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "error"
                              :error (:error token-result)})}
      (let [token (:token token-result)
            base-query {:country_code "RU"
                        :is_handout true}
            cdek-query (cond-> base-query
                         (and (some? city-code) (not= "" (str city-code)))
                         (assoc :city_code (str city-code))
                         (not= "" (str search-text))
                         (assoc :name (str search-text))
                         type-val
                         (assoc :type type-val))
            response (cdek-request :get "/v2/deliverypoints" token {:query cdek-query})
            body (:body response)]
        {:status (or (:status response) 200)
         :headers {"Content-Type" "application/json"}
         :body (json/write-str body)}))))

(defn normalize-city [city]
  (when (map? city)
    {:name (or (:full_name city) (:name city) (get city "full_name") (get city "name"))
     :code (or (:code city) (get city "code"))
     :city_uuid (or (:city_uuid city) (get city "city_uuid"))}))

(defn cdek-search-city [req]
  (let [req-body (:params req)
        search-text (or (:search_text req-body) (:search-text req-body) "")
        token-result (cdek-token-get false)]
    (if (< (count search-text) 2)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "error"
                              :error "Минимум 2 символа для поиска"
                              :code 400})}
      (if-not (:ok token-result)
        {:status (or (:status token-result) 500)
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:status "error"
                                :error (:error token-result)})}
        (let [token (:token token-result)
              response (cdek-request :get "/v2/location/suggest/cities" token
                                     {:query {:name search-text
                                              :country_code "RU"}})
              cities (->> (:body response)
                          (map normalize-city)
                          (remove nil?)
                          vec)
              result {:status "ok"
                      :cities cities}]
          {:status (or (:status response) 200)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str result)})))))
