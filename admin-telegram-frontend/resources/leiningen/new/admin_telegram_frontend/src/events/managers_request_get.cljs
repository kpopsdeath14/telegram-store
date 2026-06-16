(ns {{name}}.events.managers-request-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn managers_request_get_handler [[ok? response]]
  (swap! app-state assoc :managers_request response) 
  )


(defn managers_request_get []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "managers-request-get")
      :method :post
      :params {}
      :handler managers_request_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))