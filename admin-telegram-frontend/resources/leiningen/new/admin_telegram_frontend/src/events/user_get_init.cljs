(ns {{name}}.events.user-get-init
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]] 
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
