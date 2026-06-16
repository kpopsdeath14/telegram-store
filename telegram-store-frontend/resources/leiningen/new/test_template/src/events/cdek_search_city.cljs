(ns {{name}}.events.cdek-search-city
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

(defn extract-cities [body]
  (cond
    (sequential? body) body
    (map? body) (or (:cities body)
                    (get body "cities")
                    [])
    :else []))

(defn cdek_search_city [search-text]
  (let [value (or search-text "")]
    (if (< (count value) 2)
      (swap! app-state assoc
             :cdek_city_suggestions []
             :cdek_city_loading? false
             :cdek_city_error nil)
      (do
        (swap! app-state assoc
               :cdek_city_loading? true
               :cdek_city_error nil)
        (http/ajax-request-with-headers
         {:uri (api_uri_maker "cdek-search-city")
          :method :post
          :params {:search_text value}
         :handler (fn [[ok? response]]
                    (let [body (normalize-body response)]
                      (if ok?
                        (if-let [err (error-body body)]
                          (do
                            (swap! app-state assoc
                                   :cdek_city_suggestions []
                                   :cdek_city_loading? false
                                   :cdek_city_error err)
                            (js/alert (str "CDEK: ошибка поиска города."
                                           "\nЗапрос: " (pr-str value)
                                           "\nОшибка: " (pr-str err))))
                          (swap! app-state assoc
                                 :cdek_city_suggestions (extract-cities body)
                                 :cdek_city_loading? false
                                 :cdek_city_error nil))
                        (do
                          (swap! app-state assoc
                                 :cdek_city_suggestions []
                                 :cdek_city_loading? false
                                 :cdek_city_error body)
                          (js/alert (str "CDEK: ошибка поиска города."
                                         "\nЗапрос: " (pr-str value)
                                         "\nОшибка: " (pr-str body))))
                        )
                        )
                        )
                        
          :format (ajax/json-request-format)
          :response-format (ajax/json-response-format {:keywords? true})
          }
          )
          )
          )
          )
          )
