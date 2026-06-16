(ns jemo-murge-admin-frontend.pages.model-unit.search
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )






(defn search []
  (let [Button antd/Button
        model_unit (reagent/cursor app-state [:model_unit])
        ]
    (fn []
      [:> Button {
                 :id "catalog_search_input"
                 :size "large"
                 :placeholder "Наименование"
                 :style {:border "1px solid #00247C"
                         :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                         :border-radius 15
                         :height 50
                         :background "#D3EAFF"
                         :width "100%"
                         :text-align "left"
                         :font-size 24
                         :font-weight 300

                         :display "flex"
                         :justify-content "space-between"
                         :align-items "center"
                         :padding "0 16px"
                         }
                  :icon (as-element [:> icons/EditOutlined])
                  :iconPosition "end"
                 :onClick (fn []
                            (swap! app-state assoc :model_unit_attribute_name "name")
                            (swap! app-state assoc-in [:current_vendor_changes :name] (:name (first @model_unit)))
                            (swap! app-state assoc :show_model_unit_editing? true) 
                            )
                 }
       (:name (first @model_unit))
       ]
      )
    )
    )
