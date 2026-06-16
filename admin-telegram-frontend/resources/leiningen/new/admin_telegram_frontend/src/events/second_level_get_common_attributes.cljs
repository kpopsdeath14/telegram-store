(ns {{name}}.events.second-level-get-common-attributes
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn second_level_get_common_attributes_handler [[ok? response]] 
  (swap! app-state assoc :second_level_common_attributes
         (if ok?
           (:data (first response))
           {}
           )
         )
  )


(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))



(defn second_level_get_common_attributes [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "model-unit-get")
      :method :post
      :params {:filters (transform_filters filters)}
      :handler second_level_get_common_attributes_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))