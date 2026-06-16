(ns {{name}}.events.owners-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn owners_get_handler [[ok? response]]
  (swap! app-state assoc :owners response)
  )


(defn owners_get []
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "owners-get")
      :method :post
      :params {}
      :handler owners_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
