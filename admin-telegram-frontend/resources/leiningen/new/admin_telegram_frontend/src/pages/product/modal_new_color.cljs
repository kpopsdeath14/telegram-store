(ns {{name}}.pages.product.modal-new-color
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [reagent.core :as reagent :refer [as-element]]
   )
  )

(defn modal_new_color []
  (let [Modal antd/Modal
        Input antd/Input
        Select antd/Select
        Button antd/Button

        visible? (reagent/cursor app-state [:show_modal_new_color])
        filters (reagent/cursor app-state [:filters]) 
        current_vendor_changes (reagent/cursor app-state [:current_vendor_changes])
        ]
    (fn []
      [:> Modal
       {:visible @visible?
        :closable true
        :width "80vw"
        :style {:top "35%"
                :maxWidth "800px"}
        :onCancel (fn []
                    (swap! app-state assoc :show_modal_new_color false)
                    (swap! app-state assoc :current_vendor_changes {}))

        :footer (fn [_ {:keys [OkBtn CancelBtn]}]
                  (as-element
                   (let []
                     [:> Button
                      {:style {:background "#D3EAFF"
                               :color "black"
                               :height 50
                               :border-radius 15
                               :width "auto"
                               :font-size 24
                               :font-weight 300
                               :box-shadow "0 2px 8px rgba(0, 4, 6, 0.25)"}
                       :disabled false
                       :onClick (fn [] 
                                  (swap! app-state assoc-in [:product_changes :color] (get-in @app-state [:current_vendor_changes :color_string]))
                                  (swap! app-state assoc :current_vendor_changes {})
                                  (swap! app-state assoc :show_modal_new_color false)
                                  )
                       }
                      "Сохранить"]
                     )
                   )
                  )
                  }

       [:div {:style {:width "100%"
                      :padding 16
                      :boxSizing "border-box"}}
        

        [:div
         [:> Input {:style {:width "100%"
                            :height 50
                            :border "1px solid #00247C"
                            :border-radius 6}
                    :placeholder "Введите новый цвет"
                    :defaultValue (:color_string @current_vendor_changes)
                    :onChange (fn [event]
                                (let [value (.-value (.-target event))]
                                  (swap! app-state
                                         assoc-in
                                         [:current_vendor_changes :color_string]
                                         value) 
                                  )
                                )
                    }
          ]
         ]
        ]
        ]
       )
       )
       )
