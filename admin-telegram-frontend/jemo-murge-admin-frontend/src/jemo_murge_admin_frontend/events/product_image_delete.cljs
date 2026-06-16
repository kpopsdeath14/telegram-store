(ns jemo-murge-admin-frontend.events.product-image-delete
  (:require [ajax.core :as ajax]
            [jemo-murge-admin-frontend.http-client :as http]
            [jemo-murge-admin-frontend.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker]]
            )
  )





(defn product_image_delete_handler [[ok? response]]
  
  )




(defn product_image_delete [image_name]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-image-delete")
      :method :post
      :params {:image_name image_name}
      :handler product_image_delete_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
