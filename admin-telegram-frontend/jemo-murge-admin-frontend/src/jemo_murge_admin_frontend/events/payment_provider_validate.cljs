(ns jemo-murge-admin-frontend.events.payment-provider-validate
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )

(defn payment_provider_validate_handler [on-success on-error]
  (fn [[ok? response]]
    (if (and ok? (:ok response))
      (when on-success (on-success response))
      (when on-error (on-error response)))
    )
  )

(defn payment_provider_validate [payment_provider_name provider_token on-success on-error]
  (http/ajax-request-with-headers
   {:uri (api_uri_maker "payment-provider-validate")
    :method :post
    :params {:payment_provider_name payment_provider_name
             :provider_token provider_token}
    :handler (payment_provider_validate_handler on-success on-error)
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))
