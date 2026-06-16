(ns {{name}}.events.archive-product
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            [{{name}} .events.units-get :refer [units_get]]
            )
  )



(defn archive_product_handler [[ok? response]]
  nil
  )


(defn archive_product [actual vendor_code product_ids]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "archive-product")
      :method :post
      :params {:actual actual :vendor_codes vendor_code :product_ids product_ids}
      :handler archive_product_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
