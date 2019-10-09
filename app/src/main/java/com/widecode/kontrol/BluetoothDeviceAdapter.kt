package com.widecode.kontrol

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView


class BluetoothDeviceAdapter(private val context: Context, private val dataSource: ArrayList<BluetoothDevice>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItem(position: Int): Any {
        return dataSource[position].name
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        if (convertView == null) {
            view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        } else {
            view = convertView
        }

        val device = getItem(position) as String
        val listRow = view.findViewById<TextView>(android.R.id.text1)
        listRow.text = device

        return view
    }

}