import unittest
from check_compile import preprocess

class StonecutterPreprocessTests(unittest.TestCase):
    def test_replacements_are_bidirectional_and_include_substrings(self):
        source='Identifier ResourceLocation AvatarRenderer PlayerRenderer getPlayerRenderer'
        self.assertEqual('ResourceLocation ResourceLocation PlayerRenderer PlayerRenderer getPlayerRenderer',preprocess(source,'1.21.1','fabric'))
        self.assertEqual('Identifier Identifier AvatarRenderer AvatarRenderer getAvatarRenderer',preprocess(source,'1.21.11','fabric'))
    def test_inactive_loader_and_version_branches_are_stripped_before_replacement(self):
        source='''//? if fabric {
//? if >=1.21.11 {
AvatarRenderer
//?} else {
/*PlayerRenderer
*///?}
//?}
'''
        self.assertEqual('AvatarRenderer',preprocess(source,'1.21.11','fabric'))
        self.assertEqual('PlayerRenderer',preprocess(source,'1.21.1','fabric'))
        self.assertEqual('',preprocess(source,'1.21.1','forge'))

if __name__=='__main__':unittest.main()
