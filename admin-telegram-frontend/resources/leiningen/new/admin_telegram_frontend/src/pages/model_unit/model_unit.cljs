(ns {{name}}.pages.model-unit.model-unit
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.pages.model-unit.options :refer [options]]
   [{{name}}.pages.model-unit.search :refer [search]]
   [{{name}}.pages.model-unit.filters :refer [filters]]
   [{{name}}.pages.model-unit.model-unit-product-list :refer [model_unit_product_list]]
   [{{name}}.pages.model-unit.footer :refer [footer]]
   [{{name}}.pages.model-unit.model-unit-editing :refer [model_unit_editing]]
   [{{name}}.pages.model-unit.common-values-table :refer [common_values_table]]
   [{{name}}.pages.model-unit.modal-help :refer [modal_help]]
   )
  )


(defn model_unit_page []
  (let [Button antd/Button
        
        Row (.-Row antd)
        Col (.-Col antd)

        filters_picked (reagent/cursor app-state [:filters_picked]) 
        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:model_unit_search_value])
        ]
    (fn []
      [:div {:style {:margin-bottom 141}}
       [modal_help]
       [options]
       [common_values_table]
       [model_unit_product_list]
       [footer]
       ]
      )
    )
  )