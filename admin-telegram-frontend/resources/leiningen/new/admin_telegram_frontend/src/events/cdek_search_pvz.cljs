(ns {{name}}.events.cdek-search-pvz
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]))

(defn normalize-body [response]
  (or (:body response) (get response "body") response))

(defn error-body [body]
  (when (map? body)
    (let [status (or (:status body) (get body "status"))
          error (or (:error body) (get body "error"))
          errors (or (:errors body) (get body "errors"))]
      (when (or error errors (= "error" status))
        body))))

(defn extract-pvz [body]
  (cond
    (sequential? body) body
    (map? body) (or (:pvz body)
                    (get body "pvz")
                    (:points body)
                    (get body "points")
                    (:delivery_points body)
                    (get body "delivery_points")
                    [])
    :else []))

(defn cdek_search_pvz
  ([city-code search-text on-success]
   (cdek_search_pvz city-code search-text on-success nil))
  ([city-code search-text on-success on-error]
   (let [code (str (or city-code ""))
         value (str (or search-text ""))]
     (if (empty? code)
       (when on-success (on-success []))
       (let [query (cond-> {:city_code code
                            :country_code "RU"
                            :is_handout true
                            :type "PVZ"}
                     (not= value "") (assoc :name value :search_text value))]
         (http/ajax-request-with-headers
          {:uri (api_uri_maker "cdek-delivery-points")
           :method :post
           :params {:cdek_query query}
           :handler (fn [[ok? response]]
                      (let [body (normalize-body response)]
                        (if ok?
                          (if-let [err (error-body body)]
                            (when on-error (on-error err))
                            (when on-success (on-success (extract-pvz body))))
                          (when on-error (on-error body)))))
           :format (ajax/json-request-format)
           :response-format (ajax/json-response-format {:keywords? true})}))))))
