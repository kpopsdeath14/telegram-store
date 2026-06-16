(ns {{name}}.events.product-add
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn product_add_handler [[ok? response]]
  (set! (.-href (.-location js/window)) (str "#/product/" (js/encodeURIComponent (:product_id response))))
  )



(defn product_add [common_attributes]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-add")
      :method :post
      :params {:common_attributes common_attributes}
      :handler product_add_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))