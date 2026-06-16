(ns {{name}}.events.cart-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]] 
            )
  )



(defn cart_get_handler [[ok? response]]
  (swap! app-state assoc
         :cart (if ok?
                 (vec (map (fn [product] (:data product)) response))
                 [])
         :cart_loaded? true)
  )

(defn cart_get []
  (let []
    (swap! app-state assoc :cart_loaded? false)
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "cart-get")
      :method :post
      :params {}
      :handler cart_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
