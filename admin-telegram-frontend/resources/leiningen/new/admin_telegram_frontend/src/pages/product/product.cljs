(ns {{name}}.pages.product.product
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.pages.product.options :refer [options]]
   [{{name}}.pages.product.images :refer [images]]
   [{{name}}.pages.product.parameters :refer [parameters]]
   [{{name}}.pages.product.modal-picture-edit :refer [modal_picture_edit]]
   [{{name}}.pages.product.modal-new-color :refer [modal_new_color]]
   [{{name}}.pages.product.common-parameters :refer [common_parameters]]
   [{{name}}.pages.product.price :refer [price]]
   [{{name}}.pages.product.modal-help :refer [modal_help]]
   )
  )


(defn product_page [] 
  (let [Row antd/Row
        Col antd/Col
        Button antd/Button 
        Divider antd/Divider

        product_editing? (reagent/cursor app-state [:product_editing?])
        product_changes (reagent/cursor app-state [:product_changes])
        filters (reagent/cursor app-state [:filters])
        ]
    
    (fn []
      [:div
       [modal_help]
       [options]
       [images]
       [modal_picture_edit]
       [common_parameters]
       [:> Divider]
       [price]
       [:> Divider]
       [parameters]  
       ]
      ) 
    )
  )