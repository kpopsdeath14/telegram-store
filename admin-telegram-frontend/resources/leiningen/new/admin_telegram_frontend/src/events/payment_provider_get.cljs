(ns {{name}}.events.payment-provider-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn payment_provider_get_handler [[ok? response]]
  (swap! app-state assoc :payment_methods
         (if ok?
           (mapv (fn [product] (:data product)) response)
           {}
           )
         )
  )



(defn payment_provider_get []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "payment-provider-get")
      :method :post
      :params {}
      :handler payment_provider_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
