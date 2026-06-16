(ns {{name}}.events.product-attribute-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}}.events.units-get :refer [units_get]]
            [{{name}}.events.model-unit-get :refer [model_unit_get]]
            [{{name}}.events.product-get :refer [product_get]]
            [{{name}}.events.filters-get :refer [filters_get]]
            [{{name}}.events.second-level-get-common-attributes :refer [second_level_get_common_attributes]]
            )
  )




(defn product_filter_attribute_add_handler [[ok? response]] 
  (filters_get [])
  (case (:page @app-state)
    :units (units_get (assoc {}
                             :actual (case (:products_mode @app-state)
                                       "catalog" ["t" "true"]
                                       "archive" ["f" "false"])) "")
    
    :model_unit (do
                  (model_unit_get (assoc {}
                                       :unit_id [(:current_unit_id @app-state)]
                                       :actual (case (:products_mode @app-state)
                                                 "catalog" ["t" "true"]
                                                 "archive" ["f" "false"])) "")
                  (second_level_get_common_attributes (assoc {}
                                         :unit_id [(:current_unit_id @app-state)]
                                         ))
                  )
    
    :product (product_get (assoc {}
                                 :product_id [(:current_product_id @app-state)]
                                 ) "")
    :nil)
  )



(defn product_filter_attribute_add [filters]
  (let [] 
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-filter-attribute-add")
      :method :post
      :params {:filters filters}
      :handler product_filter_attribute_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))

