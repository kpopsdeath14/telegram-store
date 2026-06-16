(ns {{name}}.events.product-image-delete
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
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