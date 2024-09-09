package dev.pranav.reef.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.github.appintro.SlidePolicy
import dev.pranav.reef.databinding.IntroDetailsBinding

class DetailsFragment(
    private val title: String,
    private val description: String,
    @DrawableRes private val imageRes: Int = 0,
    private val listener: (() -> Unit) = {},
    private val isTaskCompleted: () -> Boolean = { true }
) : Fragment(),
    SlidePolicy {

    private lateinit var binding: IntroDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IntroDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            if (imageRes != 0) {
                image.visibility = View.VISIBLE
                image.setImageResource(imageRes)
            }
            title.text = this@DetailsFragment.title
            description.text = this@DetailsFragment.description
        }
    }

    override val isPolicyRespected: Boolean
        get() = isTaskCompleted()

    override fun onUserIllegallyRequestedNextPage() {
        listener()
    }
}
