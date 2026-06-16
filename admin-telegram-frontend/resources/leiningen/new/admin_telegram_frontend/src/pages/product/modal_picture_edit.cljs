(ns {{name}}.pages.product.modal-picture-edit
  (:require
   ["antd" :as antd]
   [{{name}}.db :refer [app-state]]
   ["@ant-design/icons" :as icons]
   [reagent.core :as reagent :refer [as-element]]
   [{{name}}.api-uri-maker :refer [api_uri_maker image_uri_maker]]
   [{{name}}.http-client :as http]
   [{{name}}.events.product-attribute-add :refer [product_filter_attribute_add]]
   [{{name}}.events.product-image-delete :refer [product_image_delete]]
   ))



(defn handle-delete-image [image-name]
  (let [product (reagent/cursor app-state [:product])
        current-images (:images @product)
        updated-images (vec (remove #(= % image-name) current-images))
        filters-data {:filters [{:attribute_name "product_id"
                                 :attribute_values [(:product_id @product)]}]
                      :set_attribute_name "images"
                      :set_attribute_value updated-images}]
    (product_filter_attribute_add filters-data)
    (product_image_delete image-name)))

(defn handle-move-image [image-name direction]
  (let [product (reagent/cursor app-state [:product])
        current-images (:images @product)
        current-index (.indexOf current-images image-name)
        new-index (case direction
                    :up (dec current-index)
                    :down (inc current-index))
        final-images (-> current-images
                         (assoc current-index (nth current-images new-index))
                         (assoc new-index image-name))
        filters-data {:filters [{:attribute_name "product_id"
                                 :attribute_values [(:product_id @product)]}]
                      :set_attribute_name "images"
                      :set_attribute_value final-images}]
    (product_filter_attribute_add filters-data)))

(defn image_upload [file-list*]
  (let [Dragger antd/Upload.Dragger
        PlusOutlined icons/PlusOutlined
        UpOutlined icons/UpOutlined
        DownOutlined icons/DownOutlined
        product (reagent/cursor app-state [:product])
        upload-completed? (reagent/atom true)]

    (fn []
      (let [telegram-user-id (http/get-telegram-user-id)]
        [:div
         [:> Dragger
         {:action (api_uri_maker "picture-upload")
           :name "file"
           :accept "image/*"
           :multiple true
           :maxCount (- 10 (count (:images @product)))
           :showUploadList (not @upload-completed?)
           :fileList (clj->js (or @file-list* []))
           :headers (http/get-telegram-headers)
           :data (merge {:product_id (:product_id @product)}
                        (when telegram-user-id
                          {:telegram_user_id telegram-user-id}))
         :beforeUpload (fn [file file-list]
                         (reset! upload-completed? false)
                         true)
         :onChange (fn [info]
                     (let [file-list (.-fileList info)
                           all-finished? (->> file-list
                                              (map #(.-status %))
                                              (every? #(or (= % "done") (= % "error"))))]

                       (reset! file-list* (js->clj file-list :keywordize-keys true))

                       (when all-finished? 
                         (reset! upload-completed? true)
                         (let [successful-files (->> file-list
                                                     (filter #(= "done" (.-status %)))
                                                     (map #(.-response %)))
                               new-images (mapv #(:filename (js->clj % :keywordize-keys true)) successful-files)]

                           (when (seq new-images)
                             (let [filters-data {:filters [{:attribute_name "product_id"
                                                            :attribute_values [(:product_id @product)]}]
                                                 :set_attribute_name "images"
                                                 :set_attribute_value (into (:images @product) new-images)}]
                               (product_filter_attribute_add filters-data)
                               (reset! file-list* [])
                               )
                             )
                           )
                         )
                       )
                       )
                       }
          [:div {:style {:padding "20px"}}
           [:p.ant-upload-drag-icon
            [:> PlusOutlined]]
           [:p.ant-upload-text "Кликните или перетащите файлы для загрузки"]]]

         [:> antd/List
          {:dataSource (if-not (nil? (:images @product))
                         (:images @product)
                         [])
           :renderItem (fn [item index]
                         (let [image-url (image_uri_maker item)
                               total-count (count (:images @product))
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
           :style {:marginTop "20px"}}]]))))




(defn modal_picture_edit []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:show_modal_picture_edit?])
        file-list* (reagent/atom [])]
    (fn []
      [:> Modal
       {:visible @visible?
        :closable true
        :footer nil
        :width "80vw"
        :destroyOnClose true
        :onCancel (fn []
                   (reset! file-list* [])
                   (swap! app-state assoc :show_modal_picture_edit? false)
                   )
        }

       [:div {:style {:width "100%"
                      :padding 16
                      :boxSizing "border-box"}}
        [image_upload file-list*]
        ]
       ]
      )
    )
    )
