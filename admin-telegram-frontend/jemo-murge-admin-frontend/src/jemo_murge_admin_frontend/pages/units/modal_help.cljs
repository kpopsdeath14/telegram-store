(ns jemo-murge-admin-frontend.pages.units.modal-help
  (:require
   ["antd" :as antd]
   [jemo-murge-admin-frontend.db :refer [app-state]]
   [reagent.core :as reagent :refer [as-element]])
  )

(defn modal_help []
  (let [Modal antd/Modal
        visible? (reagent/cursor app-state [:units_help_modal_open?])
        help-type (reagent/cursor app-state [:units_help_type])
        help-content {"orders"   {:title "Заказы"
                                  :description "тут можно посмотреть заказы, оформленные в магазине. обработать их, перевести в другой статус, связаться с покупателем"}
                      "settings" {:title "Настройки каталога"
                                  :description "в этом разделе можно настроить работу приложения:\nподключить оплату\nподключить доставку\nнастроить внешний вид магазина\nи т.п."}
                      "table"    {:title "Таблица товаров"
                                  :description "в таблице ниже отображены все товары, которые есть в системе. все модификации одного товара сгруппированы в одну строку таблицы. чтобы посмотреть каждую отдельную модификацию нужно перести в товар.\n\nтовары могут быть в архиве/в каталоге. если товар в архиве, то он не будет отображаться в магазине.\n\nесли у товара количество в наличии = 0 - он тоже не будет отображаться в магазине"}
                      "archive"  {:title "Перейти в архив"
                                  :description "это переключатель между товарами в архиве и товарами в каталоге"}}]
    (fn []
      (let [current-help (get help-content @help-type {:title "" :description ""})]
        [:> Modal
         {:title (as-element [:div {:style {:font-size "18px" :font-weight 600}} (:title current-help)])
          :visible @visible?
          :closable true
          :onCancel #(swap! app-state assoc :units_help_modal_open? false)
          :footer nil
          :width 600}
         [:div {:style {:white-space "pre-wrap" :line-height "1.6" :font-size "14px"}}
          (:description current-help)]]))))
