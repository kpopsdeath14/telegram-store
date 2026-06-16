(ns jemo-murge-admin-frontend.pages.units.search
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )






(defn search []
  (let [Input antd/Input 
        
        products_mode (reagent/cursor app-state [:products_mode])
        filters_picked (reagent/cursor app-state [:filters_picked])
        search_value (reagent/cursor app-state [:search_value]) 
        ]

    (fn []
      [:> Input {:defaultValue @search_value
                 :id "catalog_search_input"
                 :size "large"
                 :placeholder "Поиск" 
                 :style {
                         :border "3px solid #D3EAFF"
                         :border-radius 15
                         :height 50 
                         :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"
                         :font-size 24
                         }
                 :onChange (fn [event]
                             (let [value (.-value (.-target event))]
                               (swap! app-state assoc :search_value value) 
                               )
                             )
                 }
       ]
      )
    )
    )
