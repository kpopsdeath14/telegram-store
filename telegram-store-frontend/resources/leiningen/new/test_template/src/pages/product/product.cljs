(ns {{name}}.pages.product.product
  (:require
   [{{name}}.pages.product.images :refer [images]]
   [{{name}}.pages.product.characteristics :refer [characteristics]]))

(defn product_page []
  (fn []
    [:div {:style {:minHeight "100vh"
                   :background "#ffffff"}}
     [images]
     [characteristics]
     ]
    )
  )
