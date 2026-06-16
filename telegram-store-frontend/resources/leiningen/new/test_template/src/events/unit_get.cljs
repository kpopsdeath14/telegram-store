(ns {{name}}.events.unit-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn unit_get_handler [[ok? response]]
  (swap! app-state assoc :unit (-> response first :_result)) 
  )


(defn unit_get [unit_id]
  (let [
        ]
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "unit-get")
      :method :post
      :params {:unit_id unit_id}
      :handler unit_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))