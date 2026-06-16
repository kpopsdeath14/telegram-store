(ns jemo-murge-admin-frontend.events.delivery-provider-add
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.delivery-provider-get :refer [delivery_provider_get]]
            )
  )

(defn delivery_provider_add_handler [[ok? response]]
  (delivery_provider_get)
  )

(defn delivery_provider_add [delivery_provider_name connection_attributes]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "delivery-provider-add")
      :method :post
      :params {:delivery_provider_name delivery_provider_name
               :connection_attributes connection_attributes}
      :handler delivery_provider_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
