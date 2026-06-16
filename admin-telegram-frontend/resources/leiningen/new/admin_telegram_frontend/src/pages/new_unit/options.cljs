(ns {{name}}.pages.new-unit.options
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [clojure.string :as string]
   [{{name}}.events.product-add :refer [product_add]]
   )
  )


(defn join [sep coll]
  (reduce (fn [acc s] (str acc (when (seq acc) sep) s)) "" coll))



(defn options []
  (let [
        Row antd/Row
        Col antd/Col
        Button antd/Button

        model_unit (reagent/cursor app-state [:model_unit])
        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:search_value]) 
        second-lvl-config (reagent/cursor app-state [:second_lvl_config])
        second-level-common-attributes (reagent/cursor app-state [:second_level_common_attributes])
        ]
    (fn []
      [:> Row {:gutter 16
               :style {:margin-bottom 25}} 
       
       [:> Col {:span 2
                :style {:display "flex"
                        :flex-direction "column"
                        :height "100%"}}
        [:> Button {:style {:background "#D3EAFF"
                            :color "black"
                            :height 50
                            :border-radius 15
                            :width "100%"
                            :font-size 24
                            :font-weight 300
                            :box-shadow "0 2px 8px rgba(0, 2, 5, 0.25)"
                            :margin-top "auto"}
                    :onClick (fn []
                               (swap! app-state assoc :second_lvl_config {})
                               (swap! app-state assoc :second_level_common_attributes {}) 
                               (set! (.-href (.-location js/window)) (str "#/units"))
                               )
                    :icon (as-element [:> icons/ArrowLeftOutlined])}]]
       
      
       [:> Col {:span 22}
        [:> Button {:type "primary"
                    :block true
                    :style {:height 50
                            :border-radius 15
                            :overflow "hidden"
                            :white-space "nowrap"
                            :backgroundColor "#D3EAFF"
                            :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                            :color "black"
                            :font-size 24
                            :font-weight 300}
                    :onClick (fn []
                               (let [
                                     common-attributes (reagent/cursor app-state [:new_unit_common_attributes])
                                     ]
                                 (product_add @common-attributes)
                                 )
                               )
                    }
         "Добавить модификацию"]] 
         ]
      
      )
      )
      )