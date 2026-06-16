(ns {{name}}.events.payment-provider-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.payment-provider-get :refer [payment_provider_get]]
            )
  )


(defn payment_provider_add_handler [[ok? response]]
  (payment_provider_get)
  )



(defn payment_provider_add [payment_provider_name connection_attributes]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "payment-provider-add")
      :method :post
      :params {:payment_provider_name payment_provider_name :connection_attributes connection_attributes}
      :handler payment_provider_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))