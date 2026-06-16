(ns {{name}}.pages.settings.modal-banner-edit
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [{{name}}.api-uri-maker :refer [api_uri_maker banner_uri_maker]]
   [{{name}}.http-client :as http]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.events.banner-add :refer [banner_add]]
   [{{name}}.events.banner-del :refer [banner_image_del]]
   )
  )

(defn handle-delete-image [image-name]
  (let [banners (reagent/cursor app-state [:banners])
        current-images (:banner_images @banners)
        updated-images (vec (remove #(= % image-name) current-images))
        filters-data {:banner_location "main_page"
                      :banner_images updated-images
                      :banner_id (:banner_id @banners)}]

    (banner_add filters-data)
    (banner_image_del image-name)
    )
  )

(defn handle-move-image [image-name direction]
  (let [banners (reagent/cursor app-state [:banners])
        current-images (:banner_images @banners)
        current-index (.indexOf current-images image-name)
        new-index (case direction
                    :up (dec current-index)
                    :down (inc current-index))
        final-images (-> current-images
                         (assoc current-index (nth current-images new-index))
                         (assoc new-index image-name))
        filters-data {:banner_location "main_page"
                      :banner_images final-images
                      :banner_id (:banner_id @banners)}]
    (banner_add filters-data)))











(defn banner_upload [file-list*]
  (let [Dragger antd/Upload.Dragger
        PlusOutlined icons/PlusOutlined
        UpOutlined icons/UpOutlined
        DownOutlined icons/DownOutlined
        banners (reagent/cursor app-state [:banners])]

    (fn []
      (let [telegram-user-id (http/get-telegram-user-id)]
        [:div
         [:> Dragger
          {:action (api_uri_maker "banner-picture-upload")
           :name "file"
           :accept "image/*"
           :multiple true
           :maxCount (- 10 (count (:images @banners)))
           :showUploadList false
           :fileList (clj->js (or @file-list* []))
           :headers (http/get-telegram-headers)
           :data (merge {}
                        (when telegram-user-id
                          {:telegram_user_id telegram-user-id}))
           :onChange (fn [info]
                       (let [file-list (.-fileList info)
                             file (.-file info)
                             status (.-status file)]
                         (reset! file-list* (js->clj file-list :keywordize-keys true))
                         (when (= status "done")
                           (let [response (js->clj (.-response file) :keywordize-keys true)
                                 filename (:filename response)]
                             (when filename
                               (let [images-now (vec (or (:banner_images @banners) []))
                                     merged (->> (conj images-now filename) distinct vec)
                                     filters-data {:banner_location "main_page"
                                                   :banner_images merged
                                                   :banner_id (:banner_id @banners)}]
                                 (banner_add filters-data)
                                 (reset! file-list* [])
                                 )
                               )
                             )
                           )
                         (when (= status "error")
                           (js/console.error "Banner upload error" (.-error file)))))}
          [:div {:style {:padding "20px"}}
           [:p.ant-upload-drag-icon
            [:> PlusOutlined]]
           [:p.ant-upload-text "Кликните или перетащите файлы для загрузки"]]]

         [:> antd/List
          {:dataSource (if-not (nil? (:banner_images @banners))
                         (:banner_images @banners)
                         [])
           :renderItem (fn [item index]
                         (let [image-url (banner_uri_maker item)
                               total-count (count (:banner_images @banners))
                               can-move-up? (> index 0)
                               can-move-down? (< index (dec total-count))]
                           (reagent/as-element
                            [:> antd/List.Item
                             {:actions [(reagent/as-element
                                         [:> antd/Button
                                          {:type "text"
                                           :danger true
                                           :onClick #(handle-delete-image item)}
                                          "Удалить"])]}
                             [:div {:style {:display "flex"
                                            :alignItems "center"
                                            :gap "15px"
                                            :width "100%"}}
                              [:div {:style {:display "flex"
                                             :flexDirection "column"
                                             :gap "5px"}}
                               (when can-move-up?
                                 [:> antd/Button
                                  {:type "text"
                                   :icon (reagent/as-element [:> UpOutlined])
                                   :onClick #(handle-move-image item :up)
                                   :style {:padding "4px"
                                           :height "auto"}}])
                               (when can-move-down?
                                 [:> antd/Button
                                  {:type "text"
                                   :icon (reagent/as-element [:> DownOutlined])
                                   :onClick #(handle-move-image item :down)
                                   :style {:padding "4px"
                                           :height "auto"}}])]

                              [:img {:src image-url
                                     :style {:width "100px"
                                             :height "100px"
                                             :objectFit "cover"
                                             :borderRadius "6px"}}]

                              [:span {:style {:flex 1
                                              :fontSize "14px"}}
                               item]]])))
           :style {:marginTop "20px"}}
          ]
         ]
        )
      )
    )
  )

(defn modal_banner_edit []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:show_modal_banner_edit?])
        banners (reagent/cursor app-state [:banners])
        images_edit (reagent/cursor app-state [:banner_images_edit])
        file-list* (reagent/atom [])]
    (fn []
      [:> Modal
       {:visible @visible?
        :closable true
        :footer nil
        :width "80vw"
        :style {:maxWidth "800px"}
        :destroyOnClose true
        :onCancel (fn []
                    (reset! file-list* [])
                    (swap! app-state assoc :show_modal_banner_edit? false))}

       [:div {:style {:width "100%"
                      :padding 16
                      :boxSizing "border-box"}}
        [banner_upload file-list*]
        ]
       ]
      )
    )
  )
