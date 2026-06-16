(ns {{name}}.events.orders-history-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            )
  )


(defn orders_history_get_handler [[ok? response]]
  (swap! app-state assoc
         :orders_history (if ok? (vec (map (fn [product] (:data product)) response)) [])
         :orders_history_loaded? true)
  )


(defn orders_history_get []
  (let []
    (swap! app-state assoc :orders_history_loaded? false)
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "orders-history-get")
      :method :post
      :params {}
      :handler orders_history_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
