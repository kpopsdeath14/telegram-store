(ns jemo-murge-admin-frontend.events.user-get-init
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]] 
            )
  )


(defn user_get_init_handler [[ok? response]]
  (swap! app-state assoc :development (:user_get_init (first response)))
  (swap! app-state assoc :login? false))


(defn user_get_init [id]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "user-get-init")
      :method :post
      :params {}
      :handler user_get_init_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
