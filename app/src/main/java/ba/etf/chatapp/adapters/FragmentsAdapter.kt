package ba.etf.chatapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import ba.etf.chatapp.fragments.ChatsFragment
import ba.etf.chatapp.fragments.ContactsFragment
import ba.etf.chatapp.fragments.FeelingsFragment

class FragmentsAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> ChatsFragment()
            1 -> ContactsFragment()
            2 -> FeelingsFragment()
            else -> ChatsFragment()
        }
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        var title: String? = null
        when (position) {
            0 -> title = "PORUKE"
            1 -> title = "KONTAKTI"
            2 -> title = "EMOCIJE"
        }
        return title
    }
}