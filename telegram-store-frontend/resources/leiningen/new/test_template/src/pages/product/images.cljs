(ns {{name}}.pages.product.images
  (:require
   ["antd" :as antd]
   ["@ant-design/icons" :as icons]
   [{{name}}.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.apiurimaker :refer [image_uri_maker]]
   [{{name}}.events.favorite-add :refer [favorite_add]]
   [{{name}}.events.favorite-del :refer [favorite_del]]
   [{{name}}.components.icons :refer [heart-icon]]))


(defn is-mobile? []
  (let [user-agent (.-userAgent (.-navigator js/window))
        mobile-regex #"(?i)android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini"]
    (boolean (re-find mobile-regex user-agent))))


(defn images []
  (let [Skeleton antd/Skeleton
        SkeletonNode (.-Node Skeleton)
        Image antd/Image
        product_current (reagent/cursor app-state [:product_current])
        favorites (reagent/cursor app-state [:favorites])
        image-loaded (reagent/atom #{})]
    (fn []
      (let [raw-images (:images @product_current)
            _ @image-loaded
            product-id (:product_id @product_current)
            favorite-ids (into #{}
                               (keep (fn [item]
                                       (or (:product_id item)
                                           (get-in item [:product_attributes :product_id])))
                                     @favorites))
            favored? (and product-id (contains? favorite-ids product-id))
            web-app (.-WebApp js/Telegram)
            platform (when web-app (.-platform web-app))
            is-tg-mobile? (contains? #{"ios" "android"} platform)
            is-mobile-device? (is-mobile?)
            is-mobile (or is-tg-mobile? is-mobile-device?)
            heart-top (if is-mobile 72 16)
            selected-color-images (cond
                                    (string? raw-images) [raw-images]
                                    (sequential? raw-images) raw-images
                                    :else [])]
        [:div {:style {:background "#ffffff"
                       :position "relative"}} 
         (if (or (= @product_current {}) (empty? selected-color-images))
           [:> SkeletonNode
            {:style {:height "100vw"
                     :width "100vw"}
             :active true}
            ]
           [:div {:class "horizontal-scroll"
                  :style {:display "flex"
                          :overflowX "auto"
                          :overflowY "hidden"
                          :scrollSnapType "x mandatory"
                          :WebkitOverflowScrolling "touch"
                          :paddingBottom 6
                          :scrollbarWidth "none"}}
            (map-indexed
             (fn [idx src]
               (let [image-key (str src "-" idx)
                     image-src (image_uri_maker src)
                     image-ready? (contains? @image-loaded image-key)]
                 ^{:key image-key}
                 [:div {:style {:flex "0 0 100%"
                                :width "100%"
                                :height "100vw"
                                :max-height "100vh"
                                :overflow "hidden"
                                :display "flex"
                                :justify-content "center"
                                :align-items "center"
                                :scrollSnapAlign "start"
                                :aspect-ratio "1/1"
                                :background "#ffffff"}}
                  [:> Image
                   {:src image-src
                    :preview false
                    :onClick (fn []
                               (swap! app-state assoc :photo_slider_index 0)
                               (swap! app-state assoc :image_preview_visible? true))
                    :onLoad (fn []
                              (swap! image-loaded conj image-key))
                    :onError (fn []
                               (swap! image-loaded conj image-key))
                    :style {:width "100%"
                            :height "100%"
                            :aspect-ratio "1/1"
                            :object-fit "contain"
                              :object-position "center"
                              :filter (if image-ready?
                                        "none"
                                        "blur(5px)")
                              :transition "filter 200ms ease"
                              :flex-shrink 0
                              :prefix {:mask nil}
                              :touch-action "pan-x pinch-zoom"}}]]))
             selected-color-images)])

         (when product-id
            [:button {:type "button"
                      :onClick (fn [e]
                                 (.stopPropagation e)
                                 (.impactOccurred (.-HapticFeedback (.-WebApp js/Telegram)) "light")
                                 (if favored?
                                   (favorite_del product-id nil)
                                   (favorite_add product-id nil)))
                      :style {:position "absolute"
                              :top heart-top
                              :right 16
                              :width 36
                              :height 36
                              :borderRadius "50%"
                              :border "1px solid #d0d0d0"
                              :background "#ffffff"
                              :display "flex"
                              :alignItems "center"
                              :justifyContent "center"
                              :padding 0
                              :cursor "pointer"
                              :zIndex 5}}
            [heart-icon {:filled? favored?
                         :size 20}]])
         ]
        )
      )
    )
  )
