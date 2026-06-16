(ns jemo-murge-admin-frontend.pages.units.footer
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.events.product-attribute-add :refer [product_filter_attribute_add]]
   )
  )

(defn footer []
  (let [
        Button antd/Button
        Checkbox antd/Checkbox

        units (reagent/cursor app-state [:units])
        selected_units_catalog (reagent/cursor app-state [:selected_units_catalog])
        ]
    (fn []
      
      [:div {:class "admin-footer"}
       [:div {:class "admin-footer__count"}
        (str "Выбрано: " (count @selected_units_catalog))
        ]
      
       [:div {:class "admin-footer__actions"}
        [:> Button {:icon (as-element [:> icons/DownloadOutlined {:style {:font-size "24px"}}])
                    :className "admin-footer__icon-btn"
                    :onClick (fn []
                               (let [filters-data {:filters [{:attribute_name "unit_id"
                                                              :attribute_values (:selected_units_catalog @app-state)}]
                                                   :set_attribute_name "actual"
                                                   :set_attribute_value "false"}]
                                 (product_filter_attribute_add filters-data)
                                 )
                               )
                    }]
        [:> Button {:icon (as-element [:> icons/UploadOutlined {:style {:font-size "24px"}}])
                    :className "admin-footer__icon-btn"
                    :onClick (fn []
                               (let [filters-data {:filters [{:attribute_name "unit_id"
                                                              :attribute_values (:selected_units_catalog @app-state)}]
                                                   :set_attribute_name "actual"
                                                   :set_attribute_value "true"}]
                                 (product_filter_attribute_add filters-data)
                                 )
                               )
                    }] 
        ]
      
      
       (let [unit-ids (keep :unit_id @units)
             total (count unit-ids)
             all-selected? (and (pos? total)
                                (= (count @selected_units_catalog) total))]
         [:div {:class "admin-footer__select-slot"}
          [:> Button {:className "admin-footer__select-btn"
                      :disabled (zero? total)
                      :onClick (fn []
                                 (if all-selected?
                                   (swap! app-state assoc :selected_units_catalog [])
                                   (swap! app-state assoc :selected_units_catalog (vec unit-ids))))}
           (if all-selected?
             "Отменить выбор"
             "Выбрать все")]])
       
       ]
      )
    )
  )
