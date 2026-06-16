(ns jemo-murge-admin-frontend.events.model-unit-get
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn model_unit_get_handler [[ok? response]] 
  (swap! app-state assoc :model_unit
         (if ok?
           (mapv (fn [product] (:data product)) response)
           []
           )
         ) 
  )


(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))



(defn model_unit_get [filters search_string]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "model-unit-get")
      :method :post
      :params {:filters (transform_filters filters) :search_string search_string}
      :handler model_unit_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
