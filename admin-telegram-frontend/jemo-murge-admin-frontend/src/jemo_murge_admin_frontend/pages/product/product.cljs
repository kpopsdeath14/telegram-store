(ns jemo-murge-admin-frontend.pages.product.product
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.pages.product.options :refer [options]]
   [jemo-murge-admin-frontend.pages.product.images :refer [images]]
   [jemo-murge-admin-frontend.pages.product.parameters :refer [parameters]]
   [jemo-murge-admin-frontend.pages.product.modal-picture-edit :refer [modal_picture_edit]]
   [jemo-murge-admin-frontend.pages.product.modal-new-color :refer [modal_new_color]]
   [jemo-murge-admin-frontend.pages.product.common-parameters :refer [common_parameters]]
   [jemo-murge-admin-frontend.pages.product.price :refer [price]]
   [jemo-murge-admin-frontend.pages.product.modal-help :refer [modal_help]]
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
