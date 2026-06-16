(ns {{name}}.events.order-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]
            )
  )


(defn order_get_handler [[ok? response]]
  (swap! app-state assoc
         :order_current (if ok?
                          (first (vec (map (fn [product] (:data product)) response)))
                          {})
         :order_loaded? true)
  )


(defn order_get [order_id]
  (let []
    (swap! app-state assoc :order_loaded? false)
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "order-get")
      :method :post
      :params {:order_id order_id}
      :handler order_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
