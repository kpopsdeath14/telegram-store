(ns jemo-murge-admin-frontend.pages.settings.banner-list
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [jemo-murge-admin-frontend.api-uri-maker :refer [api_uri_maker banner_uri_maker]]
   [reagent.core :as reagent :refer [as-element]]
   )
  )

(defn banner_list []
  (let [Image antd/Image
        banners (reagent/cursor app-state [:banners])]
    (fn []
      [:div {:style {:background-color "white"
                     :border "1px solid #f0f0f0"
                     :border-radius 18
                     :padding 20
                     :height "100%"
                     :cursor "pointer"
                     :transition "all 0.2s ease"
                     :box-shadow "0 2px 8px rgba(0, 0, 0, 0.08)"}
             :on-mouse-enter (fn [e]
                               (set! (.. e -currentTarget -style -boxShadow)
                                     "0 6px 16px rgba(0, 0, 0, 0.15)"))
             :on-mouse-leave (fn [e]
                               (set! (.. e -currentTarget -style -boxShadow)
                                     "0 2px 8px rgba(0, 0, 0, 0.08)"))
             :onClick (fn []
                        (swap! app-state assoc :show_modal_banner_edit? true))}
       [:div {:style {:display "flex"
                      :align-items "center"
                      :justify-content "space-between"
                      :gap 12}}
        [:div {:style {:display "flex"
                       :align-items "center"
                       :gap 12}}
         [:div {:style {:width 44
                        :height 44
                        :border-radius 14
                        :display "flex"
                        :align-items "center"
                        :justify-content "center"
                        :background-color "#f5f5f5"}}
          [:> icons/PictureOutlined {:style {:font-size 20
                                             :color "#1f1f1f"}}]]
         [:div
          [:div {:style {:font-size 16
                         :font-weight 600}}
           "Баннеры на главной странице"]
          [:div {:style {:font-size 12
                         :color "#8c8c8c"
                         :margin-top 4}}
           "Управление слайдами и порядком"]]]
        [:> icons/RightOutlined {:style {:color "#bfbfbf"}}]]

       [:div {:style {:display "flex"
                      :gap 8
                      :margin-top 16
                      :overflow "hidden"
                      :flex-wrap "nowrap"}}
        (if (seq (:banner_images @banners))
          (for [file (take 4 (:banner_images @banners))]
            [:> Image {:key file
                       :preview false
                       :src (banner_uri_maker file)
                       :style {:height 48
                               :width 48
                               :border "1px solid #E5E7EB"
                               :border-radius 12
                               :object-fit "cover"}}])
          [:div {:style {:font-size 12
                         :color "#9CA3AF"}}
           "Нет загруженных баннеров"])]])))
