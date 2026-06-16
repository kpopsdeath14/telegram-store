(ns jemo-murge-admin-frontend.events.product-get-to-fill
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.product-attribute-add :refer [product_filter_attribute_add]]
            )
  )


(defn product_get_to_fill_handler [[ok? response]]
  (let [other-product (first (mapv (fn [product] (:data product)) response))
        prev-data (:prev_data (:product_to_fill @app-state))
        parameters-to-fill (:parameters_to_fill (:product_to_fill @app-state))

        filters-data (->> parameters-to-fill
                          (keep (fn [param-key]
                                  (when param-key
                                    (let [new-value (get other-product param-key)]
                                      (when (some? new-value)
                                        {:filters [{:attribute_name "product_id"
                                                    :attribute_values [(:current_product_id @app-state)]}]
                                         :set_attribute_name (name param-key)
                                         :set_attribute_value new-value}))))))
        ]
    (product_filter_attribute_add filters-data)
    )
  )

(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))


(defn product_get_to_fill [filters]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-get")
      :method :post
      :params {:filters (transform_filters filters)}
      :handler product_get_to_fill_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
