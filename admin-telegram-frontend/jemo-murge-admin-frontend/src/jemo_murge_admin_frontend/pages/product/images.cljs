(ns jemo-murge-admin-frontend.pages.product.images
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [jemo-murge-admin-frontend.pages.product.options :refer [options]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [image_uri_maker]]
   )
  )

(defn images []
  (let [Image antd/Image
        Button antd/Button

        product (reagent/cursor app-state [:product])
        ]
    (fn []
      [:div {:style {:height "110px"
                     :box-sizing "border-box"
                     :display "flex"
                     :justify-content "space-between"
                     :padding "15px 30px"
                     :border-radius "15px"
                     :box-shadow "0 2px 8px 0 rgba(0, 0, 0, 0.1)"
                     :align-items "center"
                     :margin-bottom 25
                     :overflow "hidden"
                     :flex-wrap "nowrap"}
             :onClick (fn []
                        (swap! app-state assoc :show_modal_picture_edit? true)
                        )
             }
       [:div {:style {:display "flex" :align-items "center" :gap 8}}
        [:div {:style {:font-size 20
                       :font-weight 700}}
         "Изображения товара"]
        [:> Button {:shape "circle"
                    :icon (as-element [:> icons/QuestionCircleOutlined])
                    :onClick (fn []
                               (swap! app-state assoc :product_help_type "images")
                               (swap! app-state assoc :product_help_modal_open? true))}]]

       [:div {:style {:display "flex"
                      :gap "8px"
                      :overflow "hidden"
                      :flex-wrap "nowrap"}}

        (for [file (take 4 (:images @product))]
          [:> Image {:preview false
                     :src (image_uri_maker file)
                     :style {:height "80px"
                             :width "80px"
                             :margin-left "0px"
                             :border "1px solid #D3EAFF"
                             :border-radius "15px"
                             :object-fit "cover"}}])
        [:> Button {:type "button"
                    :style {:background "none"
                            :height "80px"
                            :border-radius "15px"
                            :width "80px"
                            :border "6px solid #D3EAFF"
                            :display "flex"
                            :align-items "center"
                            :justify-content "center"}
                    :onClick (fn [] nil)}
         [:> icons/PlusOutlined {:style {:font-size "32px"
                                         :color "#6BA6FF"}}]]]])))
