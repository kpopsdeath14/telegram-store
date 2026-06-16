(ns {{name}}.services.cdek
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [clojure.string :as s]
   [{{name}}.datamodule :as dm])
  )

(defonce suppress-cookie-warnings
  (doto (java.util.logging.Logger/getLogger "org.apache.http.client.protocol.ResponseProcessCookies")
    (.setLevel java.util.logging.Level/OFF)))

(def cdek-prod-url "https://api.cdek.ru")
(def cdek-test-url "https://api.edu.cdek.ru")
(def cdek-tariff-codes [136 137 138 139])
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

(defn parse-int-safe [value]
  (try
    (cond
      (number? value) (int value)
      (string? value) (Integer/parseInt value)
      :else nil)
    (catch Exception _ nil)))

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

(defn normalize-cdek-tariffs [tariffs]
  (let [has-config (and (map? tariffs) (seq tariffs))]
    (reduce
     (fn [acc code]
       (let [cfg (or (get tariffs code) (get tariffs (str code)) {})
             enabled (if has-config
                       (parse-bool (or (:enabled cfg) (get cfg "enabled")))
                       true)]
         (assoc acc code {:enabled enabled
                          :from_address (or (:from_address cfg) (get cfg "from_address"))
                          :from_pvz_code (or (:from_pvz_code cfg) (get cfg "from_pvz_code"))})))
     {}
     cdek-tariff-codes)))

(defn cdek-credentials []
  (let [provider (cdek-provider)
        attrs (:connection_attributes provider)
        account (connection-attr attrs :account)
        secure-password (connection-attr attrs :secure_password)
        tariffs-raw (or (get attrs :tariffs) (get attrs "tariffs") {})
        tariffs (normalize-cdek-tariffs tariffs-raw)]
    {:account account
     :secure_password secure-password
     :tariffs tariffs
     :provider provider}))

(defn enabled-tariff-codes [tariffs]
  (->> tariffs
       (filter (fn [[_ cfg]] (true? (:enabled cfg))))
       (mapv first)))

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

(defn normalize-tariff-codes [request tariffs]
  (let [tariff-codes (:tariff_codes request)
        tariff-code (:tariff_code request)
        parsed-codes (cond
                       (sequential? tariff-codes) (->> tariff-codes
                                                       (map parse-int-safe)
                                                       (remove nil?)
                                                       vec)
                       (string? tariff-codes) (->> (s/split tariff-codes #"[ ,]+")
                                                   (remove s/blank?)
                                                   (map parse-int-safe)
                                                   (remove nil?)
                                                   vec)
                       (number? tariff-codes) [(int tariff-codes)]
                       (number? tariff-code) [(int tariff-code)]
                       (string? tariff-code) (keep parse-int-safe [tariff-code])
                       :else nil)
        enabled (enabled-tariff-codes tariffs)
        fallback (if (seq enabled) enabled cdek-tariff-codes)]
    (cond
      (seq parsed-codes) (-> request
                             (assoc :tariff_codes parsed-codes)
                             (dissoc :tariff_code))
      :else (assoc request :tariff_codes fallback))))

(defn ensure-packages [payload]
  (let [packages (or (:packages payload) (get payload "packages"))]
    (if (seq packages)
      payload
      (assoc payload :packages [{:weight 1000}]))))

(defn cdek-history-id [req-body]
  (or (:history_id req-body)
      (str (java.util.UUID/randomUUID))))

(defn cdek-history-add! [history-id request context]
  (println "---------------------------")
  (println "[CDEK][history][add]"
           "history_id=" history-id)
  (println "[CDEK][history][add] request=" (pr-str request))
  (println "[CDEK][history][add] context=" (pr-str context))
  (println "---------------------------")
  (dm/db_query_sender "" dm/exchange_history_add_sql
                      {:service_type "cdek"
                       :request request
                       :context context
                       :history_id history-id})
  )

(defn cdek-history-upd! [history-id response]
  (println "---------------------------")
  (println "[CDEK][history][upd]"
           "history_id=" history-id)
  (println "[CDEK][history][upd] response=" (pr-str response))
  (println "---------------------------")
  (dm/db_query_sender "" dm/exchange_history_upd_sql
                      {:history_id history-id
                       :service_type "cdek"
                       :response response}))

(defn cdek-auth [req]
  (let [req-body (:params req)
        force? (parse-bool (:force req-body))]
    (let [result (cdek-token-get force?)]
      (if (:ok result)
        (let [token (:token result)
              response {:status "ok"
                        :cached (:cached? result)
                        :access_token (:access_token token)
                        :token_type (:token_type token)
                        :expires_in (:expires_in token)
                        :expires_at (:expires_at token)}]
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/write-str response)})
        (let [response {:status "error"
                        :error (:error result)
                        :code (:status result)}]
          {:status (or (:status result) 500)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str response)})))))

(defn cdek-calculate [req]
  (let [req-body (:params req)
        {:keys [tariffs]} (cdek-credentials)
        payload (normalize-tariff-codes (or (:cdek_request req-body)
                                            (dissoc req-body :telegram_user_id :history_id :force))
                                        tariffs)
        payload (ensure-packages payload)]
    (println "[CDEK] calculate payload:" (pr-str payload))
    (let [token-result (cdek-token-get false)]
      (if-not (:ok token-result)
        (do
          (println "[CDEK] calculate token error:" (pr-str token-result))
          {:status (or (:status token-result) 500)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:status "error"
                                  :error (:error token-result)})})
        (let [token (:token token-result)
              response (cdek-request :post "/v2/calculator/tarifflist" token {:body payload})
              result {:status (:status response)
                      :body (:body response)}]
          (println "[CDEK] calculate response status:" (:status response))
          (println "[CDEK] calculate response body:" (pr-str (:body response)))
          {:status (or (:status response) 200)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str result)})))))

(defn cdek-order-create [req]
  (let [req-body (:params req)
        history-id (cdek-history-id req-body)
        payload (or (:cdek_request req-body)
                    (dissoc req-body :telegram_user_id :history_id :force))
        context (merge
                 {:operation "order_create"
                  :url (str (cdek-base-url) "/v2/orders")}
                 (select-keys req-body [:telegram_user_id
                                        :order_id
                                        :payment_id
                                        :delivery_provider_name
                                        :history_id]))]
    (println "[CDEK] order_create payload:" (pr-str payload))
    (cdek-history-add! history-id payload context)
    (let [token-result (cdek-token-get false)]
      (if-not (:ok token-result)
        (do
          (println "[CDEK] order_create token error:" (pr-str token-result))
          {:status (or (:status token-result) 500)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:status "error"
                                  :error (:error token-result)
                                  :history_id history-id})})
        (let [token (:token token-result)
              response (cdek-request :post "/v2/orders" token {:body payload})
              result {:status (:status response)
                      :body (:body response)}]
          (println "[CDEK] order_create response status:" (:status response))
          (println "[CDEK] order_create response body:" (pr-str (:body response)))
          (cdek-history-upd! history-id result)
          {:status (or (:status response) 200)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str (assoc result :history_id history-id))})))))

(defn cdek-order-info [req]
  (let [req-body (:params req)
        order-uuid (or (:order_uuid req-body)
                       (:uuid req-body)
                       (get req-body "order_uuid")
                       (get req-body "uuid"))]
    (if (s/blank? (str order-uuid))
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "error"
                              :error "order_uuid is required"})}
      (let [token-result (cdek-token-get false)]
        (if-not (:ok token-result)
          {:status (or (:status token-result) 500)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str {:status "error"
                                  :error (:error token-result)})}
          (let [token (:token token-result)
                response (cdek-request :get (str "/v2/orders/" order-uuid) token {})
                result {:status (:status response)
                        :body (:body response)}]
            (println "[CDEK] order_info response status:" (:status response))
            (println "[CDEK] order_info response body:" (pr-str (:body response)))
            {:status (or (:status response) 200)
             :headers {"Content-Type" "application/json"}
             :body (json/write-str result)}))))))

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
            _ (println "[CDEK] deliverypoints request:" (pr-str cdek-query))
            response (cdek-request :get "/v2/deliverypoints" token {:query cdek-query})
            body (:body response)]
        (println "[CDEK] deliverypoints response:" (:status response) (pr-str body))
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
        search-text (or (:search_text req-body) (:search-text req-body) "")]
    (if (< (count search-text) 2)
      {:status 400
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:status "error"
                              :error "Минимум 2 символа для поиска"
                              :code 400})}
      (let [token-result (cdek-token-get false)]
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
             :body (json/write-str result)}))))))

;; ============================================================================
;; Automatic Track Number Update for Specific Order
;; ============================================================================

(defn- extract-track-number-from-response [response-body]
  "Extract CDEK tracking number from order info response"
  (let [entity (:entity response-body)
        related (some #(when (= (:type %) "order") %)
                      (:related_entities response-body))
        order-entity (or related entity response-body)]
    (or (:cdek_number order-entity)
        (:order_number order-entity)
        (:number order-entity)
        (:cdek_number entity)
        (:order_number entity)
        (:number entity)
        (:cdek_number response-body)
        (:order_number response-body)
        (:number response-body))))

(defn- set-order-track-number! [order-id track-number]
  "Update order tracking number in database"
  (when (and order-id (some? track-number))
    (try
      (dm/db_query_sender "" dm/order_track_number_set_sql
                          {:order_id order-id :track_number (str track-number)})
      (println "[CDEK][retry] Track number set for order_id=" order-id
               "track_number=" track-number)
      true
      (catch Exception e
        (println "[CDEK][retry] Failed to set track number:"
                 "order_id=" order-id
                 "error=" (.getMessage e))
        false))))

(defn- try-fetch-track-number [order-id cdek-uuid payment-id attempt]
  "Single attempt to fetch tracking number from CDEK"
  (try
    (println "[CDEK][retry] Attempt" attempt "for order_id=" order-id "uuid=" cdek-uuid)
    (let [token-result (cdek-token-get false)]
      (if-not (:ok token-result)
        (do
          (println "[CDEK][retry] Token error")
          false)
        (let [token (:token token-result)
              response (cdek-request :get (str "/v2/orders/" cdek-uuid) token {})
              body (:body response)]
          (if (= (:status response) 200)
            (let [track-number (extract-track-number-from-response body)]
              (if track-number
                (do
                  (set-order-track-number! order-id track-number)
                  (when payment-id
                    (cdek-history-upd! (str payment-id)
                                       {:retry_attempt attempt
                                        :order_uuid cdek-uuid
                                        :track_number track-number
                                        :updated_at (now-ms)}))
                  (println "[CDEK][retry] Success! Track number found:" track-number)
                  true)
                (do
                  (println "[CDEK][retry] Track number not ready yet")
                  false)))
            (do
              (println "[CDEK][retry] API error, status=" (:status response))
              false)))))
    (catch Exception e
      (println "[CDEK][retry] Exception:" (.getMessage e))
      false)))

(defn schedule-track-number-retries [order-id cdek-uuid payment-id]
  "Schedule delayed retries to fetch tracking number for specific order"
  (let [delays [5 10 20 30 60]  ; minutes
        executor (java.util.concurrent.Executors/newSingleThreadScheduledExecutor)]
    (println "[CDEK][retry] Scheduling retries for order_id=" order-id "uuid=" cdek-uuid)
    (doseq [[attempt delay] (map-indexed (fn [idx d] [(inc idx) d]) delays)]
      (.schedule executor
                 (fn []
                   (let [success? (try-fetch-track-number order-id cdek-uuid payment-id attempt)]
                     (when success?
                       (println "[CDEK][retry] Shutting down executor after success")
                       (.shutdown executor))))
                 delay
                 java.util.concurrent.TimeUnit/MINUTES))
    (println "[CDEK][retry] Scheduled" (count delays) "retry attempts")))
