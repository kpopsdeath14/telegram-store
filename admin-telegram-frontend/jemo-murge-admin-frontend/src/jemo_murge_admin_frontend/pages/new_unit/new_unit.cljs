(ns jemo-murge-admin-frontend.pages.new-unit.new-unit
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.pages.new-unit.options :refer [options]]
   [jemo-murge-admin-frontend.pages.new-unit.common-values-table :refer [common_values_table]]
   )
  )


(defn new_unit_page []
  (let [filters_picked (reagent/cursor app-state [:filters_picked])
        products_mode (reagent/cursor app-state [:products_mode])
        search_value (reagent/cursor app-state [:model_unit_search_value])
        ]
    (fn []
      [:div {:style {}}
       [options]
       [common_values_table]
       ]
      )
    )
  )
