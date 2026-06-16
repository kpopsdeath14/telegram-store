(ns {{name}}.events.cdek-search-pvz
  (:require [ajax.core :as ajax]
            [{{name}}.db :refer [app-state]]
            [{{name}}.http-client :as http]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))

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

(defn cdek_search_pvz [city-code search-text]
  (let [code (str (or city-code ""))
        value (str (or search-text ""))]
    (if (empty? code)
      (swap! app-state assoc
             :cdek_delivery_points []
             :cdek_delivery_points_loading? false
             :cdek_delivery_points_error nil)
      (do
        (swap! app-state assoc
               :cdek_delivery_points_loading? true
               :cdek_delivery_points_error nil)
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
                            (do
                              (swap! app-state assoc
                                     :cdek_delivery_points []
                                     :cdek_delivery_points_loading? false
                                     :cdek_delivery_points_error err)
                              (js/alert (str "CDEK: ошибка поиска ПВЗ."
                                             "\nГород: " (pr-str code)
                                             "\nЗапрос: " (pr-str value)
                                             "\nОшибка: " (pr-str err))))
                            (swap! app-state assoc
                                   :cdek_delivery_points (extract-pvz body)
                                   :cdek_delivery_points_loading? false
                                   :cdek_delivery_points_error nil))
                          (do
                            (swap! app-state assoc
                                   :cdek_delivery_points []
                                   :cdek_delivery_points_loading? false
                                   :cdek_delivery_points_error body)
                            (js/alert (str "CDEK: ошибка поиска ПВЗ."
                                           "\nГород: " (pr-str code)
                                           "\nЗапрос: " (pr-str value)
                                           "\nОшибка: " (pr-str body)))))))
            :format (ajax/json-request-format)
            :response-format (ajax/json-response-format {:keywords? true})}))))))
