(ns jemo-murge-admin-frontend.events.price-set
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend.events.units-get :refer [units_get]]
            [jemo-murge-admin-frontend.events.model-unit-get :refer [model_unit_get]]
            [jemo-murge-admin-frontend.events.product-get :refer [product_get]]
            )
  )



(defn price_set_handler [[ok? response]]
  (case (:page @app-state)
    :units (units_get (assoc {}
                             :actual (case (:products_mode @app-state)
                                       "catalog" ["t" "true"]
                                       "archive" ["f" "false"])) "")
  
    :model_unit (model_unit_get (assoc {}
                                       :unit_id [(:current_unit_id @app-state)]
                                       :actual (case (:products_mode @app-state)
                                                 "catalog" ["t" "true"]
                                                 "archive" ["f" "false"])) "")
  
    :product (product_get (assoc {}
                                 :product_id [(:current_product_id @app-state)]) "")
    :nil)
  )


(defn price_set [prices]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "price-set")
      :method :post
      :params {:prices prices}
      :handler price_set_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
