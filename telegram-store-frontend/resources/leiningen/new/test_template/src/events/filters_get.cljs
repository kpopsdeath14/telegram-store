(ns {{name}}.events.filters-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn filters_get_handler [[ok? response]]
  (swap! app-state assoc
         :filters (vec (map (fn [product] (:data product)) response))
         :filters_loaded? true) 
  )


(defn filters_get []
  (let []
    (when-not (:filters_loaded? @app-state)
      (swap! app-state assoc :filters_loaded? false))
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "filters-get")
      :method :post
      :params {}
      :handler filters_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
