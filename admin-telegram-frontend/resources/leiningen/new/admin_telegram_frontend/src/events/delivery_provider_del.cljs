(ns {{name}}.events.delivery-provider-del
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.delivery-provider-get :refer [delivery_provider_get]]
            )
  )

(defn delivery_provider_del_handler [[ok? response]]
  (delivery_provider_get)
  )

(defn delivery_provider_del [delivery_provider_name]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "delivery-provider-del")
      :method :post
      :params {:delivery_provider_name delivery_provider_name}
      :handler delivery_provider_del_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
