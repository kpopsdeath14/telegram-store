(ns {{name}}.pages.model-unit.footer
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.events.product-attribute-add :refer [product_filter_attribute_add]]
   )
  )

(defn footer []
  (let [Button antd/Button
        Checkbox antd/Checkbox
        selected_units (reagent/cursor app-state [:selected_units_model_unit])
        units (reagent/cursor app-state [:model_unit]) 
        ]
    (fn []
      [:div {:class "admin-footer"}
       [:div {:class "admin-footer__count"}
        (str "Выбрано: " (count @selected_units))
        ] 


       [:div {:class "admin-footer__actions"
              :style {:display "flex" :align-items "center" :gap 8}}
        [:> Button {:icon (as-element [:> icons/DownloadOutlined {:style {:font-size "24px"}}])
                    :className "admin-footer__icon-btn"
                    :onClick (fn []
                               (let [filters-data {:filters [{:attribute_name "product_id"
                                                              :attribute_values (:selected_units_model_unit @app-state)}]
                                                   :set_attribute_name "actual"
                                                   :set_attribute_value "false"
                                                   }
                                     ]
                                 (product_filter_attribute_add filters-data)
                                 )
                               )
                    }
         ]
        [:> Button {:icon (as-element [:> icons/UploadOutlined {:style {:font-size "24px"}}])
                    :className "admin-footer__icon-btn"
                    :onClick (fn []
                               (let [filters-data {:filters [{:attribute_name "product_id"
                                                              :attribute_values (:selected_units_model_unit @app-state)}]
                                                   :set_attribute_name "actual"
                                                   :set_attribute_value "true"}]
                                 (product_filter_attribute_add filters-data))
                               )
                    }]
        [:> Button {:shape "circle"
                    :icon (as-element [:> icons/QuestionCircleOutlined])
                    :onClick (fn []
                               (swap! app-state assoc :model_unit_help_type "archive_buttons")
                               (swap! app-state assoc :model_unit_help_modal_open? true))}]
        ]


       (let [product-ids (keep :product_id @units)
             total (count product-ids)
             all-selected? (and (pos? total)
                                (= (count @selected_units) total))]
         [:div {:class "admin-footer__select-slot"}
          [:> Button {:className "admin-footer__select-btn"
                      :disabled (zero? total)
                      :onClick (fn []
                                 (if all-selected?
                                   (swap! app-state assoc :selected_units_model_unit [])
                                   (swap! app-state assoc :selected_units_model_unit (vec product-ids))))}
           (if all-selected?
             "Отменить выбор"
             "Выбрать все")]])
       
       ]
       )
       )
       )
