(ns jemo-murge-admin-frontend.events.payment-provider-del
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.payment-provider-get :refer [payment_provider_get]]
            )
  )


(defn payment_provider_del_handler [[ok? response]]
  (payment_provider_get)
  )



(defn payment_provider_del [payment_provider_name]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "payment-provider-del")
      :method :post
      :params {:payment_provider_name payment_provider_name}
      :handler payment_provider_del_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
