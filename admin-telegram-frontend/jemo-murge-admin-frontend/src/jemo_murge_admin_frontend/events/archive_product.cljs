(ns jemo-murge-admin-frontend.events.archive-product
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            [jemo-murge-admin-frontend .events.units-get :refer [units_get]]
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
