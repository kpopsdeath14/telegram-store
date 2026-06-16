(ns {{name}}.events.product-get
  (:require [ajax.core :as ajax]
            [{{name}}.http-client :as http]
            [{{name}}.db :refer [app-state]]
            [reagent.cookies :as cookies]
            [{{name}}.api-uri-maker :refer [api_uri_maker]]
            )
  )


(defn- merge-product-with-draft [product draft]
  (-> (or product {})
      (merge (dissoc draft :prices))
      (update :prices merge (:prices draft))))

(defn product_get_handler [[ok? response]]
  (let [product-data (if ok?
                       (first (mapv (fn [product] (:data product)) response))
                       {})
        draft (:product_draft @app-state)]
    (swap! app-state assoc :product_server product-data)
    (swap! app-state assoc :product (merge-product-with-draft product-data draft)))
  (when (= :product (:page @app-state))
    (when-let [scroll-y (:scroll_restore_y @app-state)]
      (js/setTimeout
       (fn []
         (js/window.scrollTo 0 scroll-y)
         (swap! app-state dissoc :scroll_restore_y))
       0)))
  )

(defn transform_filters [m]
  (->> m
       (filter (fn [[_ v]] (not-empty v)))
       (mapv (fn [[k v]] {:attribute_name (name k) :attribute_values v}))))


(defn product_get [filters search_string]
  (let []
    (http/ajax-request-with-headers
     {:uri (api_uri_maker "product-get")
      :method :post
      :params {:filters (transform_filters filters)}
      :handler product_get_handler
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})})))
