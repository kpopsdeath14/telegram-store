(ns {{name}}.events.product-get-single
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.apiurimaker :refer [api_uri_maker]]))


(defn products_get_handler [[ok? response]]
  (swap! app-state assoc :product_current (-> response first :data)) 
  (js/console.log (:product_current @app-state))
  )



(defn product_get [product_id]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-get")
      :method :post
      :params {:product_id product_id}
      :handler products_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))