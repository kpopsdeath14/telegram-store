(ns {{name}}.events.products-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn products_get_handler [[ok? response]]
  (swap! app-state assoc
         :products (vec (map (fn [product] (:data product)) response))
         :products_loaded? true)
  )

(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))


(defn products_get [search_string filters order_by]
  (let []
    (when-not (:products_loaded? @app-state)
      (swap! app-state assoc :products_loaded? false))
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "products-get")
      :method :post
      :params {:search_string search_string :filters (transform_filters filters) :order_by [order_by]}
      :handler products_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
